-- This migration will ensure searching for class members filters using the qualified member name (<class_name>#<member_name>)
--  instead of (<class_name matches> OR <member_name matches>)

CREATE INDEX declaration_search_member_gist ON declaration USING gist ((class_name || '#' || member_name) gist_trgm_ops);

DROP FUNCTION search_members;

CREATE FUNCTION search_members (_class_name TEXT, _member_name TEXT, _source_id INT) returns setof search_results AS $$
    SELECT
        qualified_member,
        display_qualified_member,
        return_type,
        similarity(_class_name, class_name) * similarity(_member_name, member_name) AS similarity
    FROM
        qualified_declaration
    WHERE
        source_id = _source_id
        AND type = any(ARRAY[2, 3])
        AND (class_name || '#' || member_name) % (_class_name || '#' || _member_name)
        -- Eliminate results that were matched by either:
        --   - The member name being similar enough, but class name not matching at all (rare)
        --   - The classname being similar enough, but member name not matching at all (common)
        AND similarity (_class_name, class_name) <> 0
        AND similarity (_member_name, member_name) <> 0
$$ language sql;
