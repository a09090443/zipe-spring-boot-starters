INSERT INTO user_main (Name) VALUES('Tom');
INSERT INTO user_main (Name) VALUES('Jen');
INSERT INTO user_main (Name) VALUES('Andy');

INSERT INTO user_detail (Name, Gender) VALUES('Tom', 'M');
INSERT INTO user_detail (Name, Gender) VALUES('Jen', 'F');
INSERT INTO user_detail (Name, Gender) VALUES('Andy', 'M');

-- CUSTOM 自訂登入測試帳號（密碼為 BCrypt 雜湊，strength=10）
-- user01 / 1234
INSERT INTO user_login (LoginId, Password) VALUES('user01', '$2a$10$Y6WAl60GuH2wIULKsaRotuHGCAoYfGXmvclCEO2PrvRNQIqcb0VB2');
-- user02 / abcd
INSERT INTO user_login (LoginId, Password) VALUES('user02', '$2a$10$1Ihk8NP/mi1bxAErFUA0fu6RnY0EnuDEqYSa57VkDvxzgTrDNMgoK');
