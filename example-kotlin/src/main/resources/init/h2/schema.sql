CREATE TABLE `UserMain` (
                            `Id` int(11) NOT NULL AUTO_INCREMENT,
                            `Name` varchar(100) NOT NULL,
                            PRIMARY KEY (`Id`)
);

CREATE TABLE `UserDetail` (
                              `Name` varchar(100) NOT NULL,
                              `Gender` varchar(1) NOT NULL,
                              PRIMARY KEY (`Name`)
);
