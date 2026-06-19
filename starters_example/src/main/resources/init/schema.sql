CREATE TABLE `user_main` (
                            `Id` int(11) NOT NULL AUTO_INCREMENT,
                            `Name` varchar(100) CHARACTER SET utf8 NOT NULL,
                            PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;

CREATE TABLE `user_detail` (
                              `Name` varchar(100) NOT NULL,
                              `Gender` varchar(1) NOT NULL,
                              PRIMARY KEY (`Name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- CUSTOM 自訂登入示範用的登入帳號表（與 user_main 各自獨立，避免影響多資料來源切換測試）
-- Password 欄位儲存 BCrypt 雜湊字串（非明文）
CREATE TABLE `user_login` (
                              `LoginId`  varchar(100) NOT NULL,
                              `Password` varchar(100) NOT NULL,
                              PRIMARY KEY (`LoginId`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
