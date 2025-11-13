package com.businessLogic;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.postgres.PostgresService;
import com.postgres.models.MovieTicket;
import com.postgres.models.Movies;
import com.topics.CreateTicketRequest;
import com.topics.CreateTicketResponse;
import com.topics.MovieListRequest.Genre;

@ExtendWith(MockitoExtension.class)
public class CreateTicketRequestTest {
	@InjectMocks
	private BusinessLogic businessLogic;
	@Mock
	private PostgresService postgresService;
	@Mock
    private RestClient ticketingManagerClient; 
	private ObjectMapper objectMapper = new ObjectMapper();

	@Test
	public void createTicket1Test() {
		String JSON = """
			{
				"topicName": "CreateTicketRequest",
				"correlatorId": 5557,
				"movie": {
					"movieName": "Inception",
					"showtime": "2025-11-10T19:30:00-06:00",
					"genre": "SCIFI"
				},
				"seatNumber": "C5"
			}
			""";
		
		CreateTicketRequest request = null;	
		try{
			request = objectMapper.readValue(JSON, CreateTicketRequest.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		LocalDateTime showtime = LocalDateTime.of(2025, 11, 10, 19, 30, 0); // Nov 10, 2025 7:30 PM

		Movies movie1 = new Movies();
		movie1.setMovieName("Inception");
		movie1.setGenre(com.topics.MovieListRequest.Genre.SCIFI);
		movie1.setPrice(BigDecimal.valueOf(15.00));
		movie1.setShowtime(showtime);

		List<Movies> mockMovies = Arrays.asList(movie1);
		
		Mockito.when(postgresService.findByMovieName(request.getMovie().getMovieName()))
			.thenReturn(mockMovies);

		// Empty list to mock no matching tickets 
		List<MovieTicket> mockTicket = new ArrayList<>();

		Mockito.when(postgresService.findByNameTimeSeat(request.getMovie().getMovieName(), showtime, request.getSeatNumber()))
			.thenReturn(mockTicket);
		
		MovieTicket movieTicket = new MovieTicket();
		movieTicket.setMovieName("Inception");
		movieTicket.setGenre(Genre.SCIFI);
		movieTicket.setShowtime(showtime);
		movieTicket.setTicketId("8060000");
		movieTicket.setSeat("C5");

		when(postgresService.saveTicket(any(MovieTicket.class)))
			.thenReturn(movieTicket);
		
		// REST CLIENT MOCK FOR THE TICKET MANAGER
		RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
		RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
		RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
		when(ticketingManagerClient.post()).thenReturn(requestBodyUriSpec);
		when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
		when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
		when(requestBodySpec.retrieve()).thenReturn(responseSpec);
		when(responseSpec.toEntity(any(Class.class))).thenReturn(ResponseEntity.ok("8060000"));

		ResponseEntity<Object> httpResponse = businessLogic.processTicketRequest(request);
		CreateTicketResponse response = null;	
		try{
			response = objectMapper.readValue(httpResponse.getBody().toString(), CreateTicketResponse.class);
		} catch (Exception e) {
			e.printStackTrace();
		}

		assertNotNull(response);
		Assertions.assertEquals("Inception", response.getMovie().getMovieName());
		Assertions.assertEquals("C5", response.getSeatNumber());
		Assertions.assertEquals(8060000, response.getTicketId());

		// should return an error because the ticket exist
		mockTicket.add(movieTicket);
		Mockito.when(postgresService.findByNameTimeSeat(request.getMovie().getMovieName(), showtime, request.getSeatNumber()))
			.thenReturn(mockTicket);
		ResponseEntity<Object> httpResponse2 = businessLogic.processTicketRequest(request);
		CreateTicketResponse response2 = null;	
		try{
			@SuppressWarnings("null")
			String body = httpResponse2.getBody().toString();
			if (isString(body)) {
				// Handle the string case (you can log it, throw an exception, or handle accordingly)
				System.out.println("\n" + httpResponse2.getBody());
				// Optionally, create a default or error response here
			} else {
				response = objectMapper.readValue(body, CreateTicketResponse.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertNull(response2);
	}

	private boolean isString(String responseBody) {
		// Check if the response is a simple string (you may need more specific checks depending on your use case)
		return responseBody != null && responseBody.length() > 0 && responseBody.charAt(0) != '{';
	}
}
