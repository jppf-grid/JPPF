;; to remove revisions:

delete from text using text, page, revision where (revision.rev_page = page.page_id) AND (revision.rev_id != page.page_latest) AND (text.old_id = revision.rev_text_id)

delete from revision using page, revision where (revision.rev_page = page.page_id) AND (revision.rev_id != page.page_latest)

;; to remove unneeded users:

delete from user where user_id > 3