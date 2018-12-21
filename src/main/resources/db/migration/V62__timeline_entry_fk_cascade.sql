ALTER TABLE person_timeline_entries DROP CONSTRAINT person_timeline_entries_person_id_fkey;
ALTER TABLE person_timeline_entries DROP CONSTRAINT person_timeline_entries_timeline_entry_id_fkey;

ALTER TABLE person_timeline_entries ADD CONSTRAINT person_timeline_entries_person_id_fkey
  FOREIGN KEY (person_id) REFERENCES persons (id) ON DELETE CASCADE;

ALTER TABLE person_timeline_entries ADD CONSTRAINT person_timeline_entries_timeline_entry_id_fkey
FOREIGN KEY (timeline_entry_id) REFERENCES timeline_entries (id) ON DELETE CASCADE;


