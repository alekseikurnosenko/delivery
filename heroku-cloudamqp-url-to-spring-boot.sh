# Parse a CloudAmqp RabbitMQ URL from heroku into environment variables
# that Spring Boot understands.
# https://gist.github.com/wwerner/b0d744c5646cdbc4877353c46a4fae2b

MSG_TMP=$(echo $CLOUDAMQP_URL | cut -d':' -f2- | sed 's/^\/\///')
MSG_TMP_USER_PASS=$(echo $MSG_TMP | cut -d'@' -f1)
SPRING_RABBITMQ_USERNAME=$(echo $MSG_TMP_USER_PASS | cut -d':' -f1)
SPRING_RABBITMQ_PASSWORD=$(echo $MSG_TMP_USER_PASS | cut -d':' -f2)

MSG_TMP_HOST_VHOST=$(echo $MSG_TMP | cut -d'@' -f2-)
SPRING_RABBITMQ_HOST=$(echo $MSG_TMP_HOST_VHOST | cut -d'/' -f1)
SPRING_RABBITMQ_VIRTUAL_HOST=$(echo $MSG_TMP_HOST_VHOST | cut -d'/' -f2)

export SPRING_RABBITMQ_PORT = 5672