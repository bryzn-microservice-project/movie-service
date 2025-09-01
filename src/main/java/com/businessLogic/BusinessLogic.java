package com.businessLogic;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.topics.*;
import com.topics.Movie.Genre;
import com.topics.SeatResponse.Status;
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

    private HashMap<String, RestClient> restRouter = new HashMap<>();
    private HashMap<RestClient, String> restEndpoints = new HashMap<>();

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
        LOG.info("Sucessfully mapped the topics to their respective microservices...");
    }

    /*
     * Request handlers for the various topics, which communicate through REST
     * clients
     */
    public ResponseEntity<String> processTicketRequest(MovieTicketRequest movieRequest) {
        LOG.info("Received a MovieTicketRequest. ");

        // MovieTicket(String movieName, LocalDateTime showtime, Genre genre, String seatNumber, Double price)
        LocalDateTime timeConversion = movieRequest.getMovie().getShowtime().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        MovieTicket movieTicket = new MovieTicket(
                movieRequest.getMovie().getMovieName(),
                timeConversion,
                movieRequest.getMovie().getGenre(),
                movieRequest.getSeatNumber(),
                movieRequest.getPrice()
        );


        MovieTicket postgresSaveResponse = postgresService.save(movieTicket);
        Status ticketStatus = postgresSaveResponse.getId() != null ? Status.CONFIRMED : Status.FAILED;
        LOG.info("MovieRequest processed with status: " + ticketStatus);

        // Response is sent striaght back to the service orchestrator
        return postgresSaveResponse.getId() != null ? ResponseEntity.ok("Movie Ticket was successfully processed!")
                : ResponseEntity.status(500).body("Inernal Error Failed to process MovieRequest");
    }


    public ResponseEntity<String> processListRequest(MovieListRequest listRequest) {
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

        // Asynchronously send the post request to the API Gateway, so that we can
        // synchhronously respond to the service orchestrator
        CompletableFuture.runAsync(() -> {
            try {
                ResponseEntity<String> apiGatewayResponse = restRouter.get("MovieListResponse")
                    .post()
                    .uri(restEndpoints.get(restRouter.get("MovieListResponse")))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(response)
                    .retrieve()
                    .toEntity(String.class);

                if (apiGatewayResponse.getStatusCode().is2xxSuccessful()) {
                    LOG.info("Successfully sent MovieListResponse to API Gateway");
                } else {
                    LOG.error("Failed to send MovieListResponse to API Gateway with status code: {}",
                            apiGatewayResponse.getStatusCode());
                }
            } catch (Exception ex) {
                LOG.error("Error sending MovieListResponse to API Gateway", ex);
            }
        });

        return ResponseEntity.accepted().body("MovieListRequest was received... and is being processed.");
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

}
