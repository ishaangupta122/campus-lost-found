# How the app works

A campus lost and found. Students post what they lost or found, other students ask for found items, and an admin decides who gets them.

## Who uses it

| Person | What they can do |
|---|---|
| **Guest** (not logged in) | Look at items and search. Create an account or log in. |
| **Student** | Post lost or found reports, edit or close their own reports, ask for a found item, and see their own reports and requests. |
| **Admin** | See everything, decide who gets a found item, close reports, and see all users. Admins do not post or ask for items. |

Everyone who signs up is a student. The first admin account is created when the app is set up, and nobody can make themselves an admin.

## If you lost something

1. Log in and post a report: what it is, where you lost it, when, and a photo if you have one.
2. The report appears in the public list, and anyone can search for it.
3. Lost reports cannot be claimed. If someone finds your item, they post a separate **found** report.
4. When you get it back, or stop looking, close your report.

## If you found something

1. Post a **found** report with a description, where you found it, and a photo if you have one. It shows as **Open**.
2. The owner finds it in the list and sends a request to claim it. They write a message with a detail only the owner would know. The item now shows as **Claim pending**.
3. Several students can ask for the same item. The admin reads all the messages.
4. The admin picks one claim:
   - **Approve:** that student wins. All other waiting requests for the item are turned down automatically.
   - **Reject:** that request is turned down. If it was the last one waiting, the item goes back to **Open**.
5. The winner collects the item from the admin office, and the item is marked **Returned**.

## What the item statuses mean

| Status | Meaning |
|---|---|
| **Open** | Posted and waiting. Nobody has asked for it yet. |
| **Claim pending** | At least one student has asked for it, and the admin has not finished deciding. |
| **Returned** | The item went back to its owner. |
| **Closed** | The report was closed without a return, by its owner or an admin. |

Open and Claim pending items are shown in the public list. Returned and Closed ones are not.

```
Open  --someone asks-->  Claim pending  --admin approves, item collected-->  Returned
  ^                           |
  +--- last request turned ---+            Open or Claim pending --close--> Closed
       down
```

## Things the app does not allow

- Asking for your own report, or for a lost item.
- Asking twice for the same item.
- Asking for an item that is already returned, closed or approved for someone else.
- Marking an item returned before a claim is approved.
- Editing or closing a report that is already returned or closed.
- Deleting a report that has requests (close it instead).
- Admins posting or asking for items.
- Students opening admin pages.

## Looking after itself

- Pictures that were uploaded but never used are removed after a day.
- Pictures of returned or closed reports are removed after 30 days. The report itself stays.
- The admin dashboard shows how many items are in each status and how many requests are waiting.
