ALTER TABLE messages ADD FULLTEXT INDEX ft_messages_content (content) WITH PARSER ngram;
