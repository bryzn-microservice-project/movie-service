
INSERT INTO movie_service.tickets (movie_name, showtime, genre, seat, ticket_id, time_created)
VALUES (
    'Inception',
    '2025-11-10T19:30:00-06:00',
    'SCIFI',
    'A2',
    '8061234',
    '2025-11-9T19:30:00-06:00'
),
(
    'The Dark Knight',
    '2025-11-10T21:45:00-06:00',
    'ACTION',
    'A2',
    '8061122',
    '2025-11-9T19:30:00-06:00'
)
ON CONFLICT DO NOTHING;
