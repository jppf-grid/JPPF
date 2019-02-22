# Contributing to JPPF

Thank you very much for taking the time to contribute.

## Reporting Bugs

* Ensure the bug was not already reported by searching on GitHub under [Issues](https://github.com/jppf-grid/JPPF/issues).
* If you can't find an open issue addressing the problem, [open a new one](https://github.com/BOINC/boinc/issues/new).

## Suggesting Enhancements and new Features

* Ensure the feature or enhancement was not already requested by searching on GitHub under [Issues](https://github.com/jppf-grid/JPPF/issues).
* If don't find an open issue tackling the idea, [open a new one](https://github.com/jppf-grid/JPPF/issues/new).

## Making Pull Requests

**1. Fork and create a branch**

[Fork JPPF](https://help.github.com/articles/fork-a-repo/) and create a branch with a descriptive name.

A good branch name would be:
```
git checkout -b my_new_feature
```

**2. Implement your fix, feature or enhancement**

At this point, you're ready to make your changes! Feel free to ask for help, everyone has to start somewhere.

Development Practices:

* Commit does not contain unrelated code changes
* Code uses [atomic commits](https://www.freshconsulting.com/atomic-commits/)
* The code is tested by adding new tests or fixing existing ones
* New features and enhancements are documented. The documentation can be as a .md, we will ensure it makes it into the user guide. A good doc file name would be `MY_NEW_FEATURE.md`.

**3. Sync changes made in the original repository with your fork**

At this point, you should switch back to your master branch and [make sure it's up to date with JPPF's master branch](https://help.github.com/articles/configuring-a-remote-for-a-fork/):

```
git remote add upstream https://github.com/jppf-grid/JPPF.git
git checkout master
git pull upstream master
```

Then update your branch from your local copy of master, and push it!

```
git checkout my_new_feature
git rebase master
git push --set-upstream origin my_new_feature
```

**4. Make a Pull Request** 

Finally, go to GitHub and [make a Pull Request](https://help.github.com/articles/creating-a-pull-request-from-a-fork/).


Thanks!

The JPPF Team
