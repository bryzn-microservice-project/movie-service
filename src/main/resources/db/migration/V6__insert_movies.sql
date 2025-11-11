DO $$
DECLARE
    seat_json JSONB := '{}'::jsonb;
    row_letter CHAR;
    seat_num INT;
    ascii_code INT;
BEGIN
    -- Loop through letters A–E using ASCII codes (safer than ARRAY)
    FOR ascii_code IN ASCII('A')..ASCII('E') LOOP
        row_letter := CHR(ascii_code);
        FOR seat_num IN 1..10 LOOP
            seat_json := seat_json || jsonb_build_object(row_letter || seat_num, 'AVAILABLE');
        END LOOP;
    END LOOP;

    -- Insert first movie
    INSERT INTO movie_service.movies (movie_name, showtime, genre, price, seats)
    VALUES (
        'Inception',
        '2025-11-10T19:30:00-06:00',
        'SCIFI',
        12.50,
        seat_json
    )
    ON CONFLICT DO NOTHING;

    -- Insert second movie
    INSERT INTO movie_service.movies (movie_name, showtime, genre, price, seats)
    VALUES (
        'The Dark Knight',
        '2025-11-10T21:45:00-06:00',
        'ACTION',
        13.00,
        seat_json
    )
    ON CONFLICT DO NOTHING;

END $$;
