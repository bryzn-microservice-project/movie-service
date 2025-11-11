CREATE TABLE IF NOT EXISTS movie_service.movies (
    id BIGSERIAL PRIMARY KEY,
    movie_name VARCHAR(50) NOT NULL,
    showtime TIMESTAMP WITH TIME ZONE NOT NULL,
    genre TEXT CHECK (genre IN ('ACTION', 'COMEDY', 'DRAMA', 'HORROR', 'SCIFI', 'ROMANCE', 'THRILLER')),
    seats JSONB NOT NULL,
    price DECIMAL(5,2) CHECK (price >= 0)
);