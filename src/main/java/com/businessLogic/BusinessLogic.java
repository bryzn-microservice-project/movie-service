package com.businessLogic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.topics.*;
import com.topics.Movie.Genre;
import jakarta.annotation.PostConstruct;
import com.postgres.PostgresService;
import com.postgres.models.MovieTicket;
import com.postgres.models.Movies;

/*
 * Handles the business logic for processing various topics and utilizes 
 * REST clients to communicate with other microservices.
 */
@Service
public class BusinessLogic {
    private static final Logger LOG = LoggerFactory.getLogger(BusinessLogic.class);
    public final PostgresService postgresService;

    // REST Clients to communicate with other microservices
    private RestClient apiGatewayClient = RestClient.create();
    private RestClient ticketingManagerClient = RestClient.create();

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

    @Value("${api.gateway}")
    private String apigateway;
    @Value("${api.gateway.port}")
    private String apigatewayPort;
    private String agw;

    @Value("${ticketing.manager}")
    private String ticketManager;
    @Value("${ticketing.manager.port}")
    private String ticketManagerPort;
    private String tm;

    @PostConstruct
    public void init() {
        agw = "http://" + apigateway + ":" + apigatewayPort + "/api/v1/processTopic";
        restEndpoints.put(apiGatewayClient, agw);
        LOG.info("Business Logic initialized Api Gatway at: " + agw);
        restRouter.put("MovieListResponse", apiGatewayClient);
        restEndpoints.put(apiGatewayClient, agw);

        tm = "http://" + ticketManager + ":" + ticketManagerPort + "/api/v1/ticket";
        restEndpoints.put(ticketingManagerClient, tm);
        LOG.info("AsyncLogic initialized with Ticketing Manager at: " + tm);
    }

    public BusinessLogic(PostgresService postgresService) {
        this.postgresService = postgresService;
        mapTopicsToClient();
    }

    /*
     * Method to map topics to their respective microservices and endpoints
     * # api-gateway:8081
     * # movie-service:8082
     * # notification-service:8083
     * # payment-service:8084
     * # seating-service:8085
     * # user-management-service:8086
     * # gui-service:8087
     */
    public void mapTopicsToClient() {
        restRouter.put("MovieListResponse", apiGatewayClient);
        restEndpoints.put(apiGatewayClient, "http://api-gateway:8081/api/v1/processTopic");
        restEndpoints.put(ticketingManagerClient, "http://ticketing-manager:8088/api/v1/ticket");
        LOG.info("Sucessfully mapped the topics to their respective microservices...");
    }

    /*
     * Request handlers for the various topics, which communicate through REST
     * clients
     */
    public ResponseEntity<Object> processTicketRequest(CreateTicketRequest ticketRequest) {
        LOG.info("Received a MovieTicketRequest. ");

        // MovieTicket(String movieName, LocalDateTime showtime, Genre genre, String seatNumber, Double price)
        LocalDateTime timeConversion = ticketRequest.getMovie().getShowtime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        Movies movie = new Movies(
                ticketRequest.getMovie().getMovieName(),
                timeConversion,
                com.topics.MovieListRequest.Genre.valueOf(ticketRequest.getMovie().getGenre().name())
        );

        // Check to see if the movie and showtime exist in the DB, then check if the seat is available
        List<Movies> movieCheck = postgresService.findByMovieName(movie.getMovieName());

        // Default log message
        String logMessage = "No move by the title " + movie.getMovieName() + " was found...";
        for(Movies m : movieCheck) {
            if(m.getShowtime().isEqual(movie.getShowtime())) {
                LOG.info("The movie [" + movie.getMovieName() + "] found at the requested showtime " + movie.getShowtime());
                LOG.info("Attempting to generate a new ticket number from the Ticketing Manager...");
                ResponseEntity<String> ticketResponse = ticketingManagerClient
                                    .post()
                                    .uri(restEndpoints.get(ticketingManagerClient))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .retrieve()
                                    .toEntity(String.class);
                                    
                if(ticketResponse.getStatusCode().is2xxSuccessful()) {
                    LOG.info("Successfully generated a ticket number from the Ticketing Manager");

                    String ticket = ticketResponse.getBody();
                    CreateTicketResponse response = generateTicketResponse(ticket, ticketRequest);

                    MovieTicket movieTicket = new MovieTicket();
                    movieTicket.setMovieName(movie.getMovieName());
                    movieTicket.setShowtime(movie.getShowtime());
                    movieTicket.setGenre(com.topics.MovieListRequest.Genre.valueOf(movie.getGenre().name()));
                    movieTicket.setSeat(ticketRequest.getSeatNumber());
                    movieTicket.setTicketId(ticket);

                    // Searches the DB for an existing ticket with the same movie name, showtime, and seat
                    List<MovieTicket> exist = postgresService.findByNameTimeSeat(movieTicket.getMovieName(),
                        movieTicket.getShowtime(), movieTicket.getSeat());

                    for(MovieTicket t : exist) {
                        if (!t.getMovieName().equals(movieTicket.getMovieName())) {
                            LOG.info("Existing ticket [{}] movie name '{}' does not match requested movie '{}'",
                                     t.getTicketId(), t.getMovieName(), movieTicket.getMovieName());
                            continue;
                        }
                        if (!t.getShowtime().isEqual(movieTicket.getShowtime())) {
                            LOG.info("Existing ticket [{}] showtime '{}' does not match requested showtime '{}'",
                                     t.getTicketId(), t.getShowtime(), movieTicket.getShowtime());
                            continue;
                        }
                        if (t.getSeat().equals(movieTicket.getSeat())) {
                            logMessage = "Ticket with ID " + ticket + " already exists for movie "
                                + movie.getMovieName() + " at showtime " + movie.getShowtime()
                                + " for seat " + movieTicket.getSeat();
                            LOG.info(logMessage);
                            return ResponseEntity.status(409).body(logMessage);
                        }
                    }

                    MovieTicket postgresTicket = postgresService.saveTicket(movieTicket);
                    if(postgresTicket != null) {
                        LOG.info("Successfully saved the Movie Ticket to the Postgres DB with Ticket ID: " + postgresTicket.getTicketId());
                        return ResponseEntity.ok(response);
                    } else {
                        LOG.error("Failed to save the Movie Ticket to the Postgres DB.");
                    }
                } else {
                    LOG.error("Failed to generate a ticket number from the Ticketing Manager with status code: {}",
                            ticketResponse.getStatusCode());
                    logMessage = "Failed to generate a ticket number from the Ticketing Manager.";
                    return ResponseEntity.status(500).body(logMessage);
                }
            }
            else {
                logMessage = "The movie " + movie.getMovieName() + " does not have a showtime at " 
                    + movie.getShowtime();
                LOG.info("Movie " + movie.getMovieName() + " does not have a showtime at " 
                    + movie.getShowtime());
            }
        }
        LOG.info("Ticket request was not successful: " + logMessage);
        // Response is sent striaght back to the service orchestrator
        return ResponseEntity.status(500).body(logMessage);
    }


    public ResponseEntity<Object> processListRequest(MovieListRequest listRequest) {
        LOG.info("Received a MovieListRequest. ");

        // Prepare a response and have the request handle the list
        MovieListResponse response = new MovieListResponse();
        response.setTopicName("MovieListResponse");
        response.setCorrelatorId(listRequest.getCorrelatorId());
        
        // search by genre, showtime, or title
        if(listRequest.getGenre() != null) {
            LOG.info("Searching movies by genre: " + listRequest.getGenre());
            List<Movies> genreResponse = postgresService.findByGenre(listRequest.getGenre());
            LOG.info("Found " + genreResponse.size() + " movies that fall under that genre");

            response.setMovies(moviesToMovieList(genreResponse));
        }
        if(listRequest.getStartingShowtime() != null && listRequest.getEndingShowtime() != null) {
            LOG.info("Searching movies by showtime between: " + listRequest.getStartingShowtime()
                + " and " + listRequest.getEndingShowtime());
            LocalDateTime startingTime = listRequest.getStartingShowtime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            LocalDateTime endingTime = listRequest.getEndingShowtime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
            List<Movies> showtimeResponse = postgresService.findByShowtimeBetween(startingTime, endingTime);
            LOG.info("Found " + showtimeResponse.size() + " movies between those dates");

            response.setMovies(moviesToMovieList(showtimeResponse));
        }
        if(listRequest.getMovieName() != null) {
            LOG.info("Searching movies by title: " + listRequest.getMovieName());
            List<Movies> movieNameResponse = postgresService.findByMovieName(listRequest.getMovieName());
            LOG.info("Found " + movieNameResponse.size() + " movies with that title");

            response.setMovies(moviesToMovieList(movieNameResponse));
        }

        response.setTimestamp(new Date());
        if(response.getMovies().isEmpty()) {
            LOG.info("No movies found matching the criteria.");
        } else {
            LOG.info("MovieListRequest processed successfully with " + response.getMovies().size() + " movies found.");
        }
        return ResponseEntity.accepted().body(response);
    }

    private List<Movie> moviesToMovieList(List<Movies> movies) {
        List<Movie> movieList = new ArrayList<>();
        for(Movies m : movies) {
            Movie movie = new Movie();
            movie.setMovieName(m.getMovieName());
            movie.setGenre(Genre.valueOf(m.getGenre().name()));
            movie.setShowtime(Date.from(m.getShowtime().atZone(ZoneId.systemDefault()).toInstant()));
            movieList.add(movie);
        }
        return movieList;
    }

    private CreateTicketResponse generateTicketResponse(String ticket, CreateTicketRequest request) {
        CreateTicketResponse response = new CreateTicketResponse();
        response.setTopicName("CreateTicketResponse");
        response.setCorrelatorId(request.getCorrelatorId());
        response.setTicketId(Integer.valueOf(ticket));
        response.setSeatNumber(request.getSeatNumber());
        return response;
    }
}
