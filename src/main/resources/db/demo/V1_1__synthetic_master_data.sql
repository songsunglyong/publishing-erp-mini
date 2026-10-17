-- Only explicit demo/test profiles load these fictional records. No demo login yet.
INSERT INTO books (isbn, title, author, publisher, publication_date, list_price, active)
VALUES ('9780000000002', '시연 도서: 작은 출판의 시작', '가상 저자', '가상 출판사', '2026-01-01', 18000, TRUE),
       (NULL, '시연 도서: 봄날의 기록', '가상 작가', '가상 출판사', '2026-02-01', 15000, TRUE),
       (NULL, '시연 도서: 절판 자료', '가상 연구자', '가상 출판사', NULL, 12000, FALSE);
INSERT INTO inventory (book_id, quantity_on_hand)
SELECT b.id, 0 FROM books b LEFT JOIN inventory i ON i.book_id = b.id WHERE i.book_id IS NULL;
INSERT INTO partners (code, name, type, contact_person, email, active)
VALUES ('DEMO_STORE', '시연 책방', 'BOOKSTORE', '가상 담당자', 'store@example.invalid', TRUE),
       ('DEMO_DIST', '시연 유통', 'DISTRIBUTOR', '가상 담당자', 'dist@example.invalid', TRUE),
       ('DEMO_INACTIVE', '시연 휴업 책방', 'BOOKSTORE', NULL, NULL, FALSE);
