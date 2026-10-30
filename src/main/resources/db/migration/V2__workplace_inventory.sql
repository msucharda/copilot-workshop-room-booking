INSERT INTO buildings (id, code, name, address) VALUES
    (1, 'BA-HQ', 'Bratislava Headquarters', 'Prievozska 14, Bratislava'),
    (2, 'BR-OPS', 'Brno Operations Centre', 'Holandska 8, Brno'),
    (3, 'OS-OFF', 'Ostrava Office', '28. rijna 3346, Ostrava');

INSERT INTO floors (id, building_id, level, name) VALUES
    (1, 1, 0, 'Reception and visitors'),
    (2, 1, 1, 'Product and engineering'),
    (3, 1, 2, 'Leadership and shared services'),
    (4, 2, 0, 'Customer operations'),
    (5, 2, 1, 'Delivery teams'),
    (6, 3, 1, 'Regional operations');

INSERT INTO rooms (id, floor_id, code, name, capacity, active) VALUES
    (1, 1, 'BA-DANUBE', 'Danube', 12, TRUE),
    (2, 1, 'BA-LOBBY', 'Visitor Room', 4, TRUE),
    (3, 2, 'BA-TATRA', 'Tatra', 8, TRUE),
    (4, 2, 'BA-FATRA', 'Fatra', 6, TRUE),
    (5, 2, 'BA-FOCUS', 'Focus Room', 2, TRUE),
    (6, 3, 'BA-BOARD', 'Boardroom', 20, TRUE),
    (7, 3, 'BA-HRON', 'Hron', 6, FALSE),
    (8, 4, 'BR-SVRATKA', 'Svratka', 10, TRUE),
    (9, 4, 'BR-REVIEW', 'Review Room', 6, TRUE),
    (10, 5, 'BR-MORAVA', 'Morava', 16, TRUE),
    (11, 5, 'BR-PAIR', 'Pairing Room', 3, TRUE),
    (12, 6, 'OS-ODRA', 'Odra', 10, TRUE),
    (13, 6, 'OS-BESKYDY', 'Beskydy', 8, TRUE);

INSERT INTO room_equipment (room_id, equipment) VALUES
    (1, 'DISPLAY'), (1, 'VIDEO_CONFERENCING'), (1, 'WHITEBOARD'),
    (2, 'DISPLAY'),
    (3, 'DISPLAY'), (3, 'VIDEO_CONFERENCING'), (3, 'WHITEBOARD'),
    (4, 'DISPLAY'), (4, 'WHITEBOARD'),
    (5, 'WHITEBOARD'),
    (6, 'PROJECTOR'), (6, 'VIDEO_CONFERENCING'), (6, 'SPEAKERPHONE'),
    (7, 'DISPLAY'),
    (8, 'DISPLAY'), (8, 'VIDEO_CONFERENCING'),
    (9, 'WHITEBOARD'), (9, 'DISPLAY'),
    (10, 'PROJECTOR'), (10, 'VIDEO_CONFERENCING'), (10, 'WHITEBOARD'),
    (11, 'DISPLAY'),
    (12, 'DISPLAY'), (12, 'VIDEO_CONFERENCING'), (12, 'SPEAKERPHONE'),
    (13, 'DISPLAY'), (13, 'WHITEBOARD');
