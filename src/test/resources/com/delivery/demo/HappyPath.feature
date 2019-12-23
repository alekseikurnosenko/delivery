Feature: HappyPath

  Background:
    Given restaurant "Joes" with following dishes
      | dish   | price |
      | Burger | 6.00  |
      | Fries  | 3.50  |

  Scenario: User orders a delivery
    Given A signed-in user
    And user's basket is empty
    When user browses list of restaurants
    And user browses dishes of "Joes" restaurant
    And user adds 2 "Burger" to basket
    And user adds 1 "Fries" to basket
    Then user's basket should not be empty
    And user's basket total amount should be 15.50
    When user performs checkout

  Scenario: Some other one
    Given A signed-in user
    And user's basket is empty
