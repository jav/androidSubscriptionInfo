The purpose of this app was to explore how well I could target subscribers to a specific operator.

Trying to match either SubscriberID/name (from the SIM-card) AND or OR the IP-range to ISP.

It downloads both ip -> AS-number mappings as well ass AS-number -> AS-name mappings from http://thyme.apnic.net/current/


Todo:
* Break out the lookups to it's own library and only ask for e.g. AND/OR logic
* Refactor (less repeated/copy-paste code)
* Move as much as possible to back-ground threads and block lookups that depends on other lookups
