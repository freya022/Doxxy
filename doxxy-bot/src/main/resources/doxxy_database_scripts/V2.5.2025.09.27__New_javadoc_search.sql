CREATE INDEX declaration_source_id_idx ON declaration (source_id);

CREATE INDEX declaration_type_idx ON declaration (type);

CREATE VIEW fully_qualified_declarations AS
SELECT
    *,
    CASE
        WHEN identifier IS NULL THEN classname
        ELSE concat(classname, '#', identifier)
    END AS full_identifier
FROM
    declaration;

CREATE TYPE search_results AS (
    full_identifier TEXT,
    human_identifier TEXT,
    human_class_identifier TEXT,
    return_type TEXT,
    similarity REAL
);

CREATE FUNCTION search_declarations(_query TEXT, _source_id INT, _type INT) returns setof search_results AS $$
    SELECT
        full_identifier,
        CASE
            WHEN _type = 1 THEN classname
            ELSE human_identifier
        END as human_identifier,
        CASE
            WHEN _type = 1 THEN classname
            ELSE human_class_identifier
        END as human_class_identifier,
        return_type,
        CASE
            WHEN _type = 1 THEN similarity (_query, classname)
            ELSE similarity (_query, identifier_no_args)
        END AS similarity
    FROM
        fully_qualified_declarations
    WHERE
        source_id = _source_id
        AND type = _type
        AND CASE
            WHEN _type = 1 THEN _query % classname
            ELSE _query % identifier_no_args
        END
$$ language sql;

CREATE FUNCTION search_members(_class_name TEXT, _member_name TEXT, _source_id INT) returns setof search_results AS $$
    SELECT
        full_identifier,
        human_identifier,
        human_class_identifier,
        return_type,
        similarity(_class_name, classname) * similarity(_member_name, identifier_no_args) as similarity
    FROM
        fully_qualified_declarations
    WHERE
        source_id = _source_id
        AND type = ANY (ARRAY[2, 3])
        AND (
            _class_name % classname
            OR _member_name % identifier_no_args
        )
        -- Eliminate results that were matched by either:
        --   - The member name being similar enough, but class name not matching at all (rare)
        --   - The classname being similar enough, but member name not matching at all (common)
        AND similarity(_class_name, classname) <> 0
        AND similarity(_member_name, identifier_no_args) <> 0
$$ language sql;
