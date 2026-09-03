-- Mantém a leitura dos filtros eficiente mesmo com muitos contatos.
-- O predicado e as expressões correspondem exatamente à consulta do repositório.
DO
$$
BEGIN
    -- Em uma base nova a tabela ainda sera criada pelo Hibernate depois do Flyway.
    -- Nas bases existentes (homologacao/producao), o indice e criado nesta migracao.
    IF
EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'contatos') THEN
        EXECUTE 'CREATE INDEX IF NOT EXISTS idx_contatos_filtro_comarca_unidade
                 ON contatos (UPPER(BTRIM(comarca)), UPPER(BTRIM(unidade)))
                 WHERE NULLIF(BTRIM(comarca), '''') IS NOT NULL
                   AND NULLIF(BTRIM(unidade), '''') IS NOT NULL';
END IF;
END $$;
