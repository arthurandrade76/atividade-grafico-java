SELECT 
    (SELECT COUNT(*) FROM respostas_casos WHERE id_aluno = ?) AS respondidos, 
    (SELECT COUNT(*) FROM casos_clinicos) AS total, 
    (SELECT COUNT(DISTINCT id_aluno) FROM respostas_casos) AS alunos_ativos, 
    (SELECT COUNT(*) FROM alunos) AS total_alunos;

SELECT 'Responderam' AS status, COUNT(DISTINCT id_aluno) AS total FROM respostas_casos 
UNION 
SELECT 'Não Responderam' AS status, (SELECT COUNT(*) FROM alunos) - COUNT(DISTINCT id_aluno) AS total FROM respostas_casos;

SELECT 
    c.nome_curso, 
    COUNT(rc.id_resposta) 
FROM cursos c 
LEFT JOIN alunos a ON c.id_curso = a.id_curso 
LEFT JOIN respostas_casos rc ON a.id_aluno = rc.id_aluno 
GROUP BY c.nome_curso 
ORDER BY c.nome_curso ASC;

SELECT 
    data_resposta, 
    COUNT(id_resposta) 
FROM respostas_casos 
WHERE data_resposta >= CURRENT_DATE - INTERVAL '30 days' 
GROUP BY data_resposta 
ORDER BY data_resposta ASC;

