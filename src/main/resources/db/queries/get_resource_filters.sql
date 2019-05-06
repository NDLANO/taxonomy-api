select rf.public_id as resource_filter_id,
       r.public_id  as resource_id,
       f.public_id  as filter_id,
       re.public_id as relevance_id
from resource_filter rf
         join resource r on rf.resource_id = r.id
         join filter f on rf.filter_id = f.id
         join relevance re on rf.relevance_id = re.id;