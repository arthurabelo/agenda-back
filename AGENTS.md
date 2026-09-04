# Base44 Development Notes

- Run the editable development stack with `docker compose -f docker-compose.base44.yml up -d`.
- The Spring Boot app serves both the static frontend and same-origin API on host port 3000; PostgreSQL is internal to Compose.
- Active Directory settings use non-routable development placeholders so the public directory can run without institutional infrastructure. Admin login requires reachable TJPI AD configuration and valid user-supplied credentials.
- Verify the app with `curl -f http://localhost:3000/` and `curl -f http://localhost:3000/api/contatos`.
- Maven dependencies are cached in the `base44_maven_cache` volume. Source is bind-mounted and Spring Boot DevTools handles application restarts after compilation.
