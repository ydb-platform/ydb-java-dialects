### ORDER BY ? 

The ORDER BY clause generates SQL that uses the original
column names of the table, instead of the 
aliases that were previously specified.

```sql
select 
    student0_.studentId as studenti1_1_, 
    student0_.groupId as groupid2_1_, 
    student0_.name as name3_1_ 
from Students student0_ order by student0_.name_
```

At the moment, our solution to this problem is 
to use native SQL for queries or upgrade to Hibernate 6.


