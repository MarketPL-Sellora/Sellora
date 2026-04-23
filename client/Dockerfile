# Використовуємо Node.js для збірки
FROM node:22-alpine as build-stage
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# Використовуємо Nginx для роздачі статики
#FROM nginx:stable-alpine as production-stage
#COPY --from=build-stage /app/dist /usr/share/nginx/html
#EXPOSE 80
#CMD ["nginx", "-g", "daemon off;"]
# ... тут код з node:alpine або щось подібне ...

# --- Production stage (Nginx) ---
FROM nginx:stable-alpine
COPY --from=build-stage /app/dist /usr/share/nginx/html


COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
