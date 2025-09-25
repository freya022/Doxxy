CREATE TABLE javadoc (
    javadoc_id serial NOT NULL,
    decl_id INT NOT NULL,
    embed TEXT NOT NULL,
    javadoc_link TEXT NULL,
    PRIMARY KEY (javadoc_id),
    FOREIGN key (decl_id) REFERENCES doc ON DELETE CASCADE,
    -- A Javadoc can only be owned by a single declaration
    UNIQUE (decl_id)
);

INSERT INTO
    javadoc (decl_id, embed, javadoc_link)
SELECT
    id AS decl_id,
    embed,
    javadoc_link
FROM
    doc;

ALTER TABLE doc
DROP COLUMN embed,
DROP COLUMN javadoc_link;

ALTER TABLE doc
RENAME TO declaration;

ALTER TABLE doc_view
RENAME TO declaration_full_idents;