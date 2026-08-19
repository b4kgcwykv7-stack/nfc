import os
from app import app, db, User
with app.app_context():
    email=os.environ.get("ADMIN_EMAIL","admin@example.com").lower()
    password=os.environ.get("ADMIN_PASSWORD")
    if not password:
        raise SystemExit("Defina ADMIN_PASSWORD")
    u=User.query.filter_by(email=email).first()
    if not u:
        u=User(email=email); db.session.add(u)
    u.set_password(password); db.session.commit()
    print("Administrador configurado:", email)
