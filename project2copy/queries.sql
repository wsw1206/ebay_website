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
SELECT COUNT(*)
FROM
(SELECT COUNT(*) as c FROM ItemCategory
        GROUP BY ItemID
        HAVING c = 4)
as b;

-- Find the ID(s) of current (unsold) auction(s) with the highest bid. Remember that the data was captured at the point in time December 20th, 2001, one second after midnight, so you can use this time point to decide which auction(s) are current. Pay special attention to the current auctions without any bid.
-- 1046740686

-- SELECT ItemID FROM Item


-- Find the number of sellers whose rating is higher than 1000.
-- 3130


-- Find the number of users who are both sellers and bidders.
-- 6717

-- Find the number of categories that include at least one item with a bid of more than $100.
-- 150