ALTER TABLE declaration
ADD COLUMN javadoc_id INT;

UPDATE declaration
SET
    javadoc_id = (
        SELECT
            javadoc_id
        FROM
            javadoc
        WHERE
            decl_id = declaration.id
    );

ALTER TABLE declaration
ALTER COLUMN javadoc_id
SET NOT NULL;

ALTER TABLE declaration
ADD CONSTRAINT declaration_javadoc_id FOREIGN key (javadoc_id) REFERENCES javadoc (javadoc_id);

-- So it doesn't take ages to delete javadocs due to FK triggers
CREATE INDEX ON declaration (javadoc_id);

ALTER TABLE javadoc
DROP COLUMN decl_id;
