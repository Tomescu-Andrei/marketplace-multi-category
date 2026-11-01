INSERT INTO categories (name, slug) VALUES
    ('Electronics', 'electronics'),
    ('Books', 'books');

INSERT INTO category_attributes (category_id, name, code, data_type, required)
SELECT id, 'Brand', 'brand', 'TEXT', TRUE FROM categories WHERE slug = 'electronics';
INSERT INTO category_attributes (category_id, name, code, data_type, required)
SELECT id, 'Year', 'year', 'NUMBER', FALSE FROM categories WHERE slug = 'electronics';
INSERT INTO category_attributes (category_id, name, code, data_type, required)
SELECT id, 'Original packaging', 'original_packaging', 'BOOLEAN', FALSE FROM categories WHERE slug = 'electronics';
INSERT INTO category_attributes (category_id, name, code, data_type, required)
SELECT id, 'Author', 'author', 'TEXT', TRUE FROM categories WHERE slug = 'books';
