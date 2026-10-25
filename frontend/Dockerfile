FROM nginx:alpine
ENV BACKEND_URL=http://backend:8080
COPY dist/eventhub-frontend /usr/share/nginx/html
COPY nginx.conf /etc/nginx/templates/default.conf.template
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
