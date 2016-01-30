-- Find the number of users in the database.
-- 13422
SELECT COUNT(*) FROM User;
-- Find the number of items in "New York", (i.e., items whose location is exactly the string "New York"). Pay special attention to case sensitivity. You should match the items in "New York" but not in "new york".
-- 103
SELECT COUNT(*) 
FROM Item left join User
ON Item.UserID = User.UserID
where BINARY Location = 'New York'; 
-- Find the number of auctions belonging to exactly four categories.
-- 8365
SELECT COUNT(*) FROM (
SELECT COUNT(*) AS c FROM ItemCategory
GROUP BY ItemID) K
WHERE c = 4;
-- Find the ID(s) of current (unsold) auction(s) with the highest bid. Remember that the data was captured at the point in time December 20th, 2001, one second after midnight, so you can use this time point to decide which auction(s) are current. Pay special attention to the current auctions without any bid.
-- 1046740686
SELECT distinct Item.ItemID
FROM Item left join Bid on Item.ItemID = Bid.ItemID
where Ends > '2001-12-20 00:00:01'
and Amount = (SELECT max(Amount) from Bid);


-- Find the number of sellers whose rating is higher than 1000.
-- 3130
SELECT COUNT(distinct Item.UserID) FROM Item INNER join User ON Item.UserID = User.UserID
WHERE User.Rating > 1000;
-- Find the number of users who are both sellers and bidders.
-- 6717
SELECT COUNT(DISTINCT Item.UserID)
FROM Item
INNER JOIN Bid
ON Item.UserID = Bid.UserID;
-- Find the number of categories that include at least one item with a bid of more than $100.
-- 150
SELECT COUNT(distinct ItemCategory.Category)
FROM ItemCategory INNER JOIN Bid
ON ItemCategory.ItemID = Bid.ItemID
WHERE Bid.Amount > 100;
