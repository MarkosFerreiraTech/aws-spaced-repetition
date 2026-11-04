-- =====================================================================
-- V2 - Dados de demonstracao (ambiente de dev/demo)
-- Usuario: demo@spacedrep.local  /  senha: senha123
-- Hash BCrypt (custo 10) gerado fora do codigo; trocar em producao.
-- =====================================================================

INSERT INTO user_account (email, password_hash, display_name) VALUES
  ('demo@spacedrep.local',
   '$2a$10$zF/Bf853xxJveH7hTN7zTeIWYz3o3VZPEI4Uotf8Mv4Uu3WgdLfuq',
   'Usuario Demo');

INSERT INTO exam_source (user_id, name, provider, raw_text_hash)
SELECT id, 'AWS SAA-C03 Practice Exam 1', 'UDEMY',
       repeat('a', 64)
FROM user_account WHERE email = 'demo@spacedrep.local';

INSERT INTO question (exam_source_id, topic, stem, explanation, multi_select)
SELECT es.id, q.topic, q.stem, q.explanation, q.multi_select
FROM exam_source es
CROSS JOIN (VALUES
  ('IAM',
   'A company needs to grant an EC2 instance temporary credentials to read objects from an S3 bucket. Which solution is the MOST secure?',
   'An IAM role attached to the instance profile provides temporary credentials rotated automatically by AWS. Access keys embedded in the instance are long-lived and a security risk.',
   FALSE),
  ('VPC',
   'Which TWO components are required for a public subnet to reach the internet? (Choose two.)',
   'A public subnet needs an Internet Gateway attached to the VPC and a route table entry targeting it (0.0.0.0/0). A NAT Gateway is for private subnets.',
   TRUE),
  ('S3',
   'A solutions architect must store infrequently accessed data for 90 days with millisecond access. Which storage class is MOST cost-effective?',
   'S3 Standard-Infrequent Access keeps millisecond latency with lower storage cost, and the 90-day minimum duration aligns with the requirement.',
   FALSE)
) AS q(topic, stem, explanation, multi_select)
WHERE es.name = 'AWS SAA-C03 Practice Exam 1';

-- Alternativas da questao 1 (IAM)
INSERT INTO answer_option (question_id, position, text, is_correct)
SELECT q.id, v.position, v.text, v.is_correct
FROM question q
CROSS JOIN (VALUES
  (1, 'Store access keys in the EC2 user data script and rotate them weekly.', FALSE),
  (2, 'Attach an IAM role to the instance profile and use temporary credentials.', TRUE),
  (3, 'Create an IAM user with programmatic access and embed the keys in the application.', FALSE),
  (4, 'Use the root account access keys restricted by an S3 bucket policy.', FALSE)
) AS v(position, text, is_correct)
WHERE q.topic = 'IAM';

-- Alternativas da questao 2 (VPC) - multipla resposta
INSERT INTO answer_option (question_id, position, text, is_correct)
SELECT q.id, v.position, v.text, v.is_correct
FROM question q
CROSS JOIN (VALUES
  (1, 'An Internet Gateway attached to the VPC.', TRUE),
  (2, 'A NAT Gateway in the public subnet.', FALSE),
  (3, 'A route table entry pointing 0.0.0.0/0 to the Internet Gateway.', TRUE),
  (4, 'A VPC peering connection to another VPC.', FALSE)
) AS v(position, text, is_correct)
WHERE q.topic = 'VPC';

-- Alternativas da questao 3 (S3)
INSERT INTO answer_option (question_id, position, text, is_correct)
SELECT q.id, v.position, v.text, v.is_correct
FROM question q
CROSS JOIN (VALUES
  (1, 'S3 Glacier Deep Archive.', FALSE),
  (2, 'S3 Standard-Infrequent Access (S3 Standard-IA).', TRUE),
  (3, 'S3 One Zone-Infrequent Access (S3 One Zone-IA).', FALSE),
  (4, 'S3 Intelligent-Tiering with deep archive access tier.', FALSE)
) AS v(position, text, is_correct)
WHERE q.topic = 'S3';

-- Cards iniciais: todos vencendo hoje (estado novo, EF 2.50)
INSERT INTO review_card (user_id, question_id, due_date)
SELECT ua.id, q.id, CURRENT_DATE
FROM user_account ua
CROSS JOIN question q
WHERE ua.email = 'demo@spacedrep.local';
