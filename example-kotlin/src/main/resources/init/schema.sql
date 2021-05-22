CREATE TABLE `UserMain` (
                            `Id` int(11) NOT NULL AUTO_INCREMENT,
                            `Name` varchar(100) CHARACTER SET utf8 NOT NULL,
                            PRIMARY KEY (`Id`)
) ENGINE=InnoDB AUTO_INCREMENT=0 DEFAULT CHARSET=utf8;

CREATE TABLE `UserDetail` (
                              `Name` varchar(100) NOT NULL,
                              `Gender` varchar(1) NOT NULL,
                              PRIMARY KEY (`Name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
