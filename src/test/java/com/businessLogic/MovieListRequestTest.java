package com.businessLogic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;
import java.util.Date;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postgres.PostgresService;
import com.postgres.models.Movies;
import com.topics.MovieListRequest;
import com.topics.MovieListResponse;

@ExtendWith(MockitoExtension.class)
public class MovieListRequestTest {
	@InjectMocks
	private BusinessLogic businessLogic;

	@Mock
	private PostgresService postgresService;

	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void movieGenreRequestTest() {
		String JSON = """
			{
				"topicName": "MovieListRequest",
				"correlatorId": 5555,
				"genre": "ACTION"
			}
			""";
		
		MovieListRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, MovieListRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LocalDateTime showtime = LocalDateTime.now().plusDays(1);

		Movies movie1 = new Movies();
		movie1.setMovieName("FAKE_MOVIE_1");
		movie1.setGenre(com.topics.MovieListRequest.Genre.ACTION);
		movie1.setPrice(BigDecimal.valueOf(15.00));
		movie1.setShowtime(showtime);

		Movies movie2 = new Movies();
		movie2.setMovieName("FAKE_MOVIE_2");
		movie2.setGenre(com.topics.MovieListRequest.Genre.ACTION);
		movie2.setPrice(BigDecimal.valueOf(15.00));
		movie2.setShowtime(showtime);

		List<Movies> mockMovies = Arrays.asList(movie1, movie2);
		
		Mockito.when(postgresService.findByGenre(request.getGenre()))
			.thenReturn(mockMovies);

		ResponseEntity<Object> httpResponse = businessLogic.processListRequest(request);

		MovieListResponse response = null;	
		try{
			@SuppressWarnings("null")
			String body = httpResponse.getBody().toString();
			if (isString(body)) {
				// Handle the string case (you can log it, throw an exception, or handle accordingly)
				System.out.println("\n" + httpResponse.getBody());
				// Optionally, create a default or error response here
			} else {
				response = objectMapper.readValue(body, MovieListResponse.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertNotNull(response);
		Assertions.assertEquals(2, response.getMovies().size());
		Assertions.assertEquals("FAKE_MOVIE_1", response.getMovies().get(0).getMovieName());
		Assertions.assertEquals("FAKE_MOVIE_2", response.getMovies().get(1).getMovieName());
	}

	@Test
	public void movieNameRequestTest() {
		String JSON = """
			{
				"topicName": "MovieListRequest",
				"correlatorId": 5557,
				"movieName": "Inception"
			}
			""";
		
		MovieListRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, MovieListRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LocalDateTime showtime1 = LocalDateTime.of(2025, 1, 10, 15, 0, 0); // Jan 10, 2025 3:00 PM
		LocalDateTime showtime2 = LocalDateTime.of(2025, 1, 11, 19, 30, 0); // Jan 11, 2025 7:30 PM

		Movies movie1 = new Movies();
		movie1.setMovieName("Inception");
		movie1.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie1.setPrice(BigDecimal.valueOf(15.00));
		movie1.setShowtime(showtime1);

		Movies movie2 = new Movies();
		movie2.setMovieName("Inception");
		movie2.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie2.setPrice(BigDecimal.valueOf(22.00));
		movie2.setShowtime(showtime2);

		List<Movies> mockMovies = Arrays.asList(movie1, movie2);
		
		Mockito.when(postgresService.findByMovieName(request.getMovieName()))
			.thenReturn(mockMovies);

		ResponseEntity<Object> httpResponse = businessLogic.processListRequest(request);

		MovieListResponse response = null;	
		try{
			@SuppressWarnings("null")
			String body = httpResponse.getBody().toString();
			if (isString(body)) {
				// Handle the string case (you can log it, throw an exception, or handle accordingly)
				System.out.println("\n" + httpResponse.getBody());
				// Optionally, create a default or error response here
			} else {
				response = objectMapper.readValue(body, MovieListResponse.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		Date date1 = Date.from(showtime1.atZone(ZoneId.systemDefault()).toInstant());
		Date date2 = Date.from(showtime2.atZone(ZoneId.systemDefault()).toInstant());

		assertNotNull(response);
		Assertions.assertEquals(2, response.getMovies().size());
		Assertions.assertEquals("Inception", response.getMovies().get(0).getMovieName());
		Assertions.assertEquals(date1, response.getMovies().get(0).getShowtime());
		Assertions.assertEquals("Inception", response.getMovies().get(1).getMovieName());
		Assertions.assertEquals(date2, response.getMovies().get(1).getShowtime());
	}

	@Test
	public void movieShowtimeRequestTest() {
		String JSON = """
			{
				"topicName": "MovieListRequest",
				"correlatorId": 5556,
				"startingShowtime": "2025-11-10T00:00:00-06:00",
				"endingShowtime": "2025-11-12T23:59:59-06:00"
			}
			""";
		
		MovieListRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, MovieListRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LocalDateTime showtime1 = LocalDateTime.of(2025, 11, 10, 15, 0, 0); // Nov 10, 2025 3:00 PM
		LocalDateTime showtime2 = LocalDateTime.of(2025, 11, 11, 19, 30, 0); // Nov 11, 2025 7:30 PM
		LocalDateTime showtime3 = LocalDateTime.of(2025, 11, 12, 22, 15, 0); // Nov 12, 2025 10:15 PM

		Movies movie1 = new Movies();
		movie1.setMovieName("Inception");
		movie1.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie1.setPrice(BigDecimal.valueOf(15.00));
		movie1.setShowtime(showtime1);

		Movies movie2 = new Movies();
		movie2.setMovieName("Interstellar");
		movie2.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie2.setPrice(BigDecimal.valueOf(17.50));
		movie2.setShowtime(showtime2);

		Movies movie3 = new Movies();
		movie3.setMovieName("The Matrix");
		movie3.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie3.setPrice(BigDecimal.valueOf(14.00));
		movie3.setShowtime(showtime3);

		List<Movies> mockMovies = Arrays.asList(movie1, movie2, movie3);
		
		LocalDateTime startShowtime = request.getStartingShowtime().toInstant()
			.atZone(java.time.ZoneId.systemDefault())
			.toLocalDateTime();
		LocalDateTime endShowtime = request.getEndingShowtime().toInstant()
			.atZone(java.time.ZoneId.systemDefault())
			.toLocalDateTime();

		Mockito.when(postgresService.findByShowtimeBetween(startShowtime, endShowtime))
			.thenReturn(mockMovies);

		ResponseEntity<Object> httpResponse = businessLogic.processListRequest(request);

		MovieListResponse response = null;	
		try{
			@SuppressWarnings("null")
			String body = httpResponse.getBody().toString();
			if (isString(body)) {
				// Handle the string case (you can log it, throw an exception, or handle accordingly)
				System.out.println("\n" + httpResponse.getBody());
				// Optionally, create a default or error response here
			} else {
				response = objectMapper.readValue(body, MovieListResponse.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertNotNull(response);
		Assertions.assertEquals(3, response.getMovies().size());
		Assertions.assertEquals("Inception", response.getMovies().get(0).getMovieName());
		Assertions.assertEquals("Interstellar", response.getMovies().get(1).getMovieName());
		Assertions.assertEquals("The Matrix", response.getMovies().get(2).getMovieName());
	}

	private boolean isString(String responseBody) {
		// Check if the response is a simple string (you may need more specific checks depending on your use case)
		return responseBody != null && responseBody.length() > 0 && responseBody.charAt(0) != '{';
	}
}
