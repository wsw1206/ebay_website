CREATE TABLE Location (
  ItemID INT(11) NOT NULL,
  Coordinates POINT NOT NULL,
  PRIMARY KEY (ItemID)
) ENGINE = MyISAM;

INSERT INTO Location (ItemID, Coordinates)
  SELECT ItemID, POINT(Latitude, Longitude)
  FROM (Item left join User on Item.UserID = User.UserID)
  WHERE Latitude != "" AND Longitude != "";

CREATE SPATIAL INDEX Coordinates_Index ON Location(Coordinates);