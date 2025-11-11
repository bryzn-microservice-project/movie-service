package com.postgres;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.postgres.models.MovieTicket;
import com.postgres.models.Movies;
import com.topics.MovieListRequest.Genre;
import java.time.LocalDateTime;
import java.util.List;

/*
 * NOTES: all included methods inside of JPA repository
        save(S entity)	                            Save or update an entity
        saveAll(Iterable<S> entities)	            Save multiple entities
        findById(ID id)	                            Find by primary key
        existsById(ID id)	                        Check existence by ID
        findAll()	                                Get all records
        findAllById(Iterable<ID> ids)	            Get records by list of IDs
        count()	                                    Count total records
        deleteById(ID id)	                        Delete by ID
        delete(T entity)	                        Delete a specific entity
        deleteAllById(Iterable<? extends ID> ids)	Delete multiple by ID
        deleteAll(Iterable<? extends T> entities)	Delete multiple entities
        deleteAll()	                                Delete all records
 */


// Spring Data JPA creates CRUD implementation at runtime automatically.
public interface TicketsRespository extends JpaRepository<MovieTicket, Long> {
	List<MovieTicket> findByMovieName(String movieName);
    List<MovieTicket> findByShowtimeBetween(LocalDateTime start, LocalDateTime end);
    List<MovieTicket> findByGenre(Genre genre);
    List<MovieTicket> findByTicketId(String ticketId);

    @Query("""
    SELECT a
    FROM MovieTicket a
    WHERE a.movieName = :movieName
      AND a.showtime = :showtime
      AND a.seat = :seat
    """)
    List<MovieTicket> findByNameTimeSeat(@Param("movieName") String movieName, @Param("showtime") LocalDateTime showtime, @Param("seat") String seat);
}