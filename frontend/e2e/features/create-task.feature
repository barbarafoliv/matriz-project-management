# Test plan T12 (docs/test-plan.md): US-01, US-02, US-04, US-07.
Feature: Create tasks
  As a project member, I want to create tasks,
  so that the work to be done is recorded (US-04).

  Scenario: A new task appears in the To Do column without reloading the page
    Given I am signed in as "alice@example.com"
    And I have created the project "E2E project"
    When I create the task "E2E task"
    Then I see "E2E task" in the "To Do" column
    And the "In Progress" and "Done" columns are empty
