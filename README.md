################################################################
#                                                              #
#                       MOVIE-SERVICE                          #
#                                                              #
################################################################

MOVIE LIST REQUEST

Genre
{
  "topicName": "MovieListRequest",
  "correlatorId": 5555,
  "genre": "ACTION"
}

{
  "topicName": "MovieListRequest",
  "correlatorId": 5556,
  "startingShowtime": "2025-11-10T00:00:00-06:00",
  "endingShowtime": "2025-11-12T23:59:59-06:00"
}

{
  "topicName": "MovieListRequest",
  "correlatorId": 5557,
  "movieName": "Inception"
}


CREATE TICKET REQUEST
GOOD (TRY 2x for dup tickets)
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

BAD (no show time)
{
  "topicName": "CreateTicketRequest",
  "correlatorId": 5557,
  "movie": {
    "movieName": "The Dark Knight",
    "showtime": "2025-11-10T19:30:00-06:00",
    "genre": "ACTION"
  },
  "seatNumber": "D8"
}

SELECT * FROM movie_service.movies;
SELECT * FROM movie_service.tickets;