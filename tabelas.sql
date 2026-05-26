CREATE TABLE cursos (
    id_curso SERIAL PRIMARY KEY,
    nome_curso VARCHAR(100) NOT NULL
);

CREATE TABLE alunos (
    id_aluno SERIAL PRIMARY KEY,
    nome VARCHAR(150) NOT NULL,
    id_curso INT REFERENCES cursos(id_curso)
);

CREATE TABLE casos_clinicos (
    id_caso SERIAL PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao TEXT NOT NULL
);

CREATE TABLE respostas_casos (
    id_resposta SERIAL PRIMARY KEY,
    id_aluno INT REFERENCES alunos(id_aluno),
    id_caso INT REFERENCES casos_clinicos(id_caso),
    data_resposta DATE NOT NULL
);
