Feature: HappyPath

  Scenario: User orders a delivery
    Given A signed-in user
    And user's basket is empty
    When user browses list of available restaurants

  Scenario: Some other one
    Given A signed-in user
    And user's basket is empty
