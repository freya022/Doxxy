-- classname -> class_name
ALTER TABLE declaration
RENAME COLUMN classname TO class_name;

-- identifier_no_args -> member_name
ALTER TABLE declaration
RENAME COLUMN identifier_no_args TO member_name;

-- human_identifier ('methodName(Type arg1, Type arg2)') -> display_method_args ('(Type arg1, Type arg2)')
ALTER TABLE declaration
RENAME COLUMN human_identifier TO display_method_args;

UPDATE declaration
SET
    display_method_args = substr(
        display_method_args,
        strpos(display_method_args, '(')
    )
WHERE
    type = 2;

UPDATE declaration
SET
    display_method_args = NULL
WHERE
    type <> 2;

-- method_args ('(Type1, Type2)') = identifier.drop(identifier.indexOf('('))
ALTER TABLE declaration
ADD COLUMN method_args TEXT;

UPDATE declaration
SET
    method_args = substr(identifier, strpos(identifier, '('))
WHERE
    type = 2;

-- Drop the old view, remove old columns and add new view
DROP VIEW fully_qualified_declarations;

ALTER TABLE declaration
DROP COLUMN identifier;

ALTER TABLE declaration
DROP COLUMN human_class_identifier;

-- Add display_qualified_member ('MyClass#myMethod(Type1 arg1, Type2 arg2)')
CREATE VIEW qualified_declaration AS
SELECT
    *,
    CASE
        WHEN type = 1 THEN class_name
        ELSE concat(class_name, '#', member_name, method_args)
    END AS qualified_member,
    CASE
        WHEN type = 1 THEN NULL
        ELSE concat(class_name, '#', member_name, display_method_args)
    END AS display_qualified_member
FROM
    declaration;

-- Update types and functions
DROP FUNCTION search_declarations;

DROP FUNCTION search_members;

DROP TYPE search_results;

CREATE TYPE search_results AS (
    qualified_member TEXT,
    display_qualified_member TEXT,
    return_type TEXT,
    similarity REAL
);

CREATE FUNCTION search_declarations (_query TEXT, _source_id INT, _type INT) returns setof search_results AS $$
    SELECT
        qualified_member,
        CASE
            WHEN _type = 1 THEN class_name
            ELSE display_qualified_member
        END as display_qualified_member,
        return_type,
        CASE
            WHEN _type = 1 THEN similarity (_query, class_name)
            ELSE similarity (_query, member_name)
        END AS similarity
    FROM
        qualified_declaration
    WHERE
        source_id = _source_id
        AND type = _type
        AND CASE
            WHEN _type = 1 THEN _query % class_name
            ELSE _query % member_name
        END
$$ language sql;

CREATE FUNCTION search_members (
    _class_name TEXT,
    _member_name TEXT,
    _source_id INT
) returns setof search_results AS $$
    SELECT
        qualified_member,
        display_qualified_member,
        return_type,
        similarity(_class_name, class_name) * similarity(_member_name, member_name) as similarity
    FROM
        qualified_declaration
    WHERE
        source_id = _source_id
        AND type = ANY (ARRAY[2, 3])
        AND (
            _class_name % class_name
            OR _member_name % member_name
        )
        -- Eliminate results that were matched by either:
        --   - The member name being similar enough, but class name not matching at all (rare)
        --   - The classname being similar enough, but member name not matching at all (common)
        AND similarity(_class_name, class_name) <> 0
        AND similarity(_member_name, member_name) <> 0
$$ language sql;
