CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE tag
(
    guildId     bigint    NOT NULL,
    ownerId     bigint    NOT NULL,
    createdAt   timestamp NOT NULL DEFAULT now(),
    name        text      NOT NULL,
    description text      NOT NULL,
    content     text      NOT NULL,
    uses        int       NOT NULL DEFAULT 0 CHECK (uses >= 0),

    PRIMARY KEY (guildId, name)
);

CREATE TABLE javadoc (
    javadoc_id serial NOT NULL,
    embed TEXT NOT NULL,

    PRIMARY KEY (javadoc_id)
);

CREATE TABLE declaration
(
    id                     serial,
    source_id              int  NOT NULL,
    type                   int  NOT NULL,
    class_name             text NOT NULL,
    member_name            text CHECK (length(member_name) <= 100),            --For search purposes, "method"
    method_args            text,
    return_type            text,
    display_method_args    text CHECK (length(display_method_args) <= 100),    --For class-specific choice name, "(Type name, name2)"
    javadoc_id             int  NOT NULL,
    source_link            text,

    PRIMARY KEY (id),
    FOREIGN KEY (javadoc_id) REFERENCES javadoc (javadoc_id),
    UNIQUE (source_id, class_name, member_name, method_args)
);

CREATE INDEX declaration_source_id_idx ON declaration (source_id);

CREATE INDEX declaration_type_idx ON declaration (type);

-- So it doesn't take ages to delete javadocs due to FK triggers
CREATE INDEX declaration_javadoc_id_idx ON declaration (javadoc_id);

CREATE INDEX declaration_class_name_gist ON declaration USING gist (class_name gist_trgm_ops);
CREATE INDEX declaration_member_name_gist ON declaration USING gist (member_name gist_trgm_ops);

CREATE TABLE DocSeeAlsoReference
(
    id             serial,
    doc_id         int  NOT NULL,
    text           text NOT NULL,
    link           text NOT NULL,
    target_type    int  NOT NULL,
    full_signature text,

    PRIMARY KEY (id),
    FOREIGN KEY (doc_id) REFERENCES declaration ON DELETE CASCADE
);

CREATE INDEX see_also_doc_id_index ON docseealsoreference (doc_id);

--------------- Implementation metadata ---------------

-- Table containing any class
CREATE TABLE class
(
    id           serial NOT NULL,
    source_id    int    NOT NULL,
    class_type   int    NOT NULL,
    package_name text   NOT NULL,
    class_name   text   NOT NULL,
    source_link  text   NOT NULL,

    PRIMARY KEY (id)
);

-- Subclass relations
CREATE TABLE subclass
(
    superclass_id int NOT NULL,
    subclass_id   int NOT NULL,

    PRIMARY KEY (superclass_id, subclass_id),
    FOREIGN KEY (superclass_id) REFERENCES class ON DELETE CASCADE,
    FOREIGN KEY (subclass_id) REFERENCES class ON DELETE CASCADE
);

-- **Declared** methods, i.e. does not contain inherited methods
CREATE TABLE method
(
    id          serial NOT NULL,
    class_id    int    NOT NULL,
    method_type int    NOT NULL,
    name        text   NOT NULL,
    signature   text   NOT NULL,
    source_link text   NOT NULL,

    PRIMARY KEY (id),
    FOREIGN KEY (class_id) REFERENCES class ON DELETE CASCADE
);

-- Link between class class id (for example the ID of VoiceChannelManager),
--  a (potentially inherited) method ID, to it's implementation (in a superclass)
-- Typical usage would be getting a method implementation such as:
--      VoiceChannelManager#removePermissionOverride
--      Get `implementation` by VoiceChannelManager's ID (join with `class`)
--      Then get the implementation data by joining `method` with `implementation_id`
--      and finally the implementation owner (`class`) with `class_id`
CREATE TABLE implementation
(
    class_id          int NOT NULL,
    method_id         int NOT NULL,
    implementation_id int NOT NULL,

    PRIMARY KEY (class_id, method_id, implementation_id),
    FOREIGN KEY (class_id)  REFERENCES class ON DELETE CASCADE,
    FOREIGN KEY (method_id) REFERENCES method ON DELETE CASCADE,
    FOREIGN KEY (implementation_id) REFERENCES method ON DELETE CASCADE
);

CREATE TABLE library_version
(
    id          SERIAL NOT NULL,
    group_id    TEXT   NOT NULL,
    artifact_id TEXT   NOT NULL,
    classifier  TEXT   NULL,
    version     TEXT   NOT NULL,
    source_url  TEXT   NULL,

    PRIMARY KEY (id),
    CONSTRAINT library_coordinates_key UNIQUE NULLS NOT DISTINCT (group_id, artifact_id, classifier)
);

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

CREATE FUNCTION search_members (_class_name TEXT, _member_name TEXT, _source_id INT) returns setof search_results AS $$
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
