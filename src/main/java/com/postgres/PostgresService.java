package com.postgres;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.postgres.models.Movies;
import com.topics.MovieListRequest.Genre;
import com.postgres.models.MovieTicket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class PostgresService {

    @Autowired
    private MovieTicketRepository movieTicketRepository;

    @Autowired
    private MoviesRepository movieRepository;

    public Optional<MovieTicket> findById(Long id) {
        return movieTicketRepository.findById(id);
    }

    // save includes creating and updating
    public MovieTicket save(MovieTicket seat) {
        return movieTicketRepository.save(seat);
    }

    public void deleteById(Long id) {
        movieTicketRepository.deleteById(id);
    }


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
}
