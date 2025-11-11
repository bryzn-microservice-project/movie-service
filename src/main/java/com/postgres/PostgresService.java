package com.postgres;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.postgres.models.MovieTicket;
import com.postgres.models.Movies;
import com.topics.MovieListRequest.Genre;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class PostgresService {
    @Autowired
    private MoviesRepository movieRepository;
    @Autowired
    private TicketsRespository ticketsRespository;
    /*
     * 
     * METHODS RELATED TO MOVIE LIST REQUESTS
     */
    public List<Movies> findAll() {
        return movieRepository.findAll();
    }

    public List<Movies> findByMovieName(String movieName) {
        return movieRepository.findByMovieName(movieName);
    }

    public List<Movies> findByGenre(Genre genre) {
        return movieRepository.findByGenre(genre);
    }

    public List<Movies> findByShowtimeBetween(LocalDateTime starting, LocalDateTime ending) {
        return movieRepository.findByShowtimeBetween(starting, ending);
    }

    public Movies save(Movies movie) {
        return movieRepository.save(movie);
    }


    /*
     * METHODS RELATED TO MOVIE TICKET REQUESTS
     */
    @Transactional
    public MovieTicket saveTicket(MovieTicket ticket) {
        return ticketsRespository.save(ticket);
    }

    public List<MovieTicket> findByTicketId(String ticketId) {
        return ticketsRespository.findByTicketId(ticketId);
    }

    public List<MovieTicket> findByNameTimeSeat(String movieName, LocalDateTime showtime, String seat) {
        return ticketsRespository.findByNameTimeSeat(movieName, showtime, seat);
    }
}
