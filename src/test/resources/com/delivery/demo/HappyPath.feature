Feature: HappyPath

  Background:
    Given restaurant "Joes" located near "PointA" with following dishes
      | dish   | price |
      | Burger | 6.00  |
      | Fries  | 3.50  |
    Given a courier "Jake"
    And "Jake" is on shift
    And "Jake" updated his location to be near "PointB"
    And a courier "Mike"
    And "Mike" is on shift
    And "Mike" updated his location to be near "PointA"

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
    Then "Mike" is assigned to deliver this order
    Then "Joes" receives this order

  Scenario: Some other one
    Given A signed-in user
    And user's basket is empty
