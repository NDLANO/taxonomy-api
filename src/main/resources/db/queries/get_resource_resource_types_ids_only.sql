select rrt.public_id as resource_resource_type_id, r.public_id as resource_id, rt.public_id as resource_type_id
from resource_resource_type rrt
         join resource r on rrt.resource_id = r.id
         join resource_type rt on rrt.resource_type_id = rt.id;