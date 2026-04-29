FROM node:20 AS build
ARG UMI_APP_API_BASE_URL
ENV UMI_APP_API_BASE_URL=$UMI_APP_API_BASE_URL
WORKDIR /app
COPY . .
RUN npm install
RUN npm run build

FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]