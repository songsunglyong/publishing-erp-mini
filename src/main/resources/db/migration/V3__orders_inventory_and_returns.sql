CREATE TABLE sales_orders (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_no VARCHAR(40) NOT NULL UNIQUE,
 request_key VARCHAR(36) NOT NULL UNIQUE,
 partner_id BIGINT NOT NULL,
 partner_name VARCHAR(150) NOT NULL,
 ordered_at DATETIME(6) NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('DRAFT','CONFIRMED','FULFILLED','CANCELLED')),
 notes VARCHAR(500) NULL,
 total_amount BIGINT NOT NULL CHECK(total_amount BETWEEN 0 AND 999999999999999),
 created_by BIGINT NOT NULL,
 fulfilled_at DATETIME(6) NULL,
 updated_at DATETIME(6) NOT NULL,
 FOREIGN KEY(partner_id) REFERENCES partners(id),
 FOREIGN KEY(created_by) REFERENCES erp_users(id),
 INDEX ix_orders_status_date(status,ordered_at,id),
 INDEX ix_orders_fulfilled(fulfilled_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE order_items (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 book_id BIGINT NOT NULL,
 book_title VARCHAR(200) NOT NULL,
 quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 1000000),
 unit_price BIGINT NOT NULL CHECK(unit_price BETWEEN 0 AND 999999999999),
 line_total BIGINT NOT NULL CHECK(line_total >= 0 AND line_total=quantity*unit_price),
 returned_quantity INT NOT NULL DEFAULT 0 CHECK(returned_quantity BETWEEN 0 AND quantity),
 FOREIGN KEY(order_id) REFERENCES sales_orders(id),
 FOREIGN KEY(book_id) REFERENCES books(id),
 UNIQUE KEY uk_order_book(order_id,book_id),
 INDEX ix_items_book(book_id,order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE order_returns (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 order_id BIGINT NOT NULL,
 request_key VARCHAR(36) NOT NULL UNIQUE,
 reason VARCHAR(500) NOT NULL,
 total_amount BIGINT NOT NULL CHECK(total_amount >= 0),
 actor_id BIGINT NOT NULL,
 returned_at DATETIME(6) NOT NULL,
 FOREIGN KEY(order_id) REFERENCES sales_orders(id),
 FOREIGN KEY(actor_id) REFERENCES erp_users(id),
 INDEX ix_returns_date(returned_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE return_items (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 return_id BIGINT NOT NULL,
 order_item_id BIGINT NOT NULL,
 quantity INT NOT NULL CHECK(quantity BETWEEN 1 AND 1000000),
 amount BIGINT NOT NULL CHECK(amount >= 0),
 FOREIGN KEY(return_id) REFERENCES order_returns(id),
 FOREIGN KEY(order_item_id) REFERENCES order_items(id),
 UNIQUE KEY uk_return_item(return_id,order_item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE stock_movements (
 id BIGINT PRIMARY KEY AUTO_INCREMENT,
 book_id BIGINT NOT NULL,
 type VARCHAR(20) NOT NULL CHECK(type IN ('RECEIPT','SHIPMENT','RETURN','ADJUSTMENT')),
 quantity INT NOT NULL CHECK(quantity != 0),
 balance_after INT NOT NULL CHECK(balance_after >= 0),
 reference_type VARCHAR(20) NOT NULL,
 reference_id BIGINT NOT NULL,
 operation_key VARCHAR(100) NOT NULL UNIQUE,
 actor_id BIGINT NULL,
 note VARCHAR(500) NOT NULL,
 created_at DATETIME(6) NOT NULL,
 FOREIGN KEY(book_id) REFERENCES books(id),
 FOREIGN KEY(actor_id) REFERENCES erp_users(id),
 INDEX ix_movements_book_date(book_id,created_at,id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- Existing balances become an opening ledger event; do not reset existing stock.
INSERT INTO stock_movements(book_id,type,quantity,balance_after,reference_type,reference_id,operation_key,actor_id,note,created_at)
SELECT book_id,'ADJUSTMENT',quantity_on_hand,quantity_on_hand,'OPENING',book_id,
 CONCAT('OPENING:',book_id),NULL,'기존 재고 이관',CURRENT_TIMESTAMP(6)
FROM inventory WHERE quantity_on_hand != 0;
