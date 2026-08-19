from flask import Flask, request, jsonify, render_template, redirect, abort
import os, re, secrets
from urllib.parse import urlparse
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash
from flask_login import LoginManager, UserMixin, login_user, logout_user, login_required, current_user

app = Flask(__name__)
DATABASE_URL = os.environ.get("DATABASE_URL", "sqlite:///nfc_pix.db")
ADMIN_TOKEN = os.environ.get("ADMIN_TOKEN", "troque-este-token")
SUPPORT_WHATSAPP = os.environ.get("SUPPORT_WHATSAPP", "5584999377777")

app.config["SQLALCHEMY_DATABASE_URI"] = DATABASE_URL
app.config["SQLALCHEMY_TRACK_MODIFICATIONS"] = False
app.config["SECRET_KEY"] = os.environ.get("SECRET_KEY", secrets.token_hex(32))
db = SQLAlchemy(app)
login_manager = LoginManager(app)
login_manager.login_view = "login"

class User(UserMixin, db.Model):
    id = db.Column(db.Integer, primary_key=True)
    email = db.Column(db.String(190), unique=True, nullable=False)
    password_hash = db.Column(db.String(255), nullable=False)
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    def set_password(self, p): self.password_hash = generate_password_hash(p)
    def check_password(self, p): return check_password_hash(self.password_hash, p)

class Client(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    name = db.Column(db.String(180), nullable=False)
    slug = db.Column(db.String(180), unique=True, nullable=False)
    pix_key = db.Column(db.String(255), nullable=False)
    merchant = db.Column(db.String(180))
    city = db.Column(db.String(100))
    whatsapp = db.Column(db.String(40))
    instagram = db.Column(db.String(180))
    amount = db.Column(db.String(40))
    created_at = db.Column(db.DateTime, server_default=db.func.now())
    updated_at = db.Column(db.DateTime, server_default=db.func.now(), onupdate=db.func.now())
    links = db.relationship("Link", backref="client", cascade="all, delete-orphan", lazy=True)

class Link(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    client_id = db.Column(db.Integer, db.ForeignKey("client.id"), nullable=False)
    title = db.Column(db.String(180), nullable=False)
    url = db.Column(db.Text, nullable=False)
    icon = db.Column(db.String(20), default="🔗")
    sort_order = db.Column(db.Integer, default=0)
    active = db.Column(db.Boolean, default=True)

class Access(db.Model):
    id = db.Column(db.Integer, primary_key=True)
    client_id = db.Column(db.Integer, db.ForeignKey("client.id"), nullable=False)
    source = db.Column(db.String(20), default="nfc")
    user_agent = db.Column(db.Text)
    created_at = db.Column(db.DateTime, server_default=db.func.now())

@login_manager.user_loader
def load_user(uid): return db.session.get(User, int(uid))

with app.app_context():
    db.create_all()

def slugify(s):
    s = (s or "").strip().lower()
    s = re.sub(r"[^a-z0-9]+", "-", s)
    return s.strip("-")

def auth():
    token = request.headers.get("X-Admin-Token") or request.args.get("token")
    return token == ADMIN_TOKEN

def clean_url(u):
    u=(u or "").strip()
    p=urlparse(u)
    if p.scheme not in ("http","https") or not p.netloc:
        raise ValueError("URL deve começar com http:// ou https://")
    return u

@app.route("/login", methods=["GET","POST"])
def login():
    if request.method == "POST":
        d = request.form
        u = User.query.filter_by(email=d.get("email","").strip().lower()).first()
        if u and u.check_password(d.get("password","")):
            login_user(u)
            return redirect("/")
        return render_template("login.html", error="E-mail ou senha inválidos.")
    return render_template("login.html")

@app.route("/logout")
@login_required
def logout():
    logout_user()
    return redirect("/login")

@app.route("/")
@login_required
def home():
    return render_template("index.html", support_whatsapp=SUPPORT_WHATSAPP)

@app.route("/p/<slug>")
def page(slug):
    c = Client.query.filter_by(slug=slug).first_or_404()
    db.session.add(Access(client_id=c.id, source="nfc", user_agent=request.headers.get("User-Agent","")))
    db.session.commit()
    links = Link.query.filter_by(client_id=c.id, active=True).order_by(Link.sort_order, Link.id).all()
    return render_template("client.html", client={
        "id":c.id,"name":c.name,"slug":c.slug,"pix_key":c.pix_key,"merchant":c.merchant,
        "city":c.city,"whatsapp":c.whatsapp,"instagram":c.instagram,"amount":c.amount
    }, links=[{"id":x.id,"title":x.title,"url":x.url,"icon":x.icon} for x in links], support_whatsapp=SUPPORT_WHATSAPP)

@app.route("/api/clients", methods=["GET"])
@login_required
def clients():
    return jsonify([client_json(c) for c in Client.query.order_by(Client.id.desc()).all()])

def client_json(c):
    return {"id":c.id,"name":c.name,"slug":c.slug,"pix_key":c.pix_key,"merchant":c.merchant,
            "city":c.city,"whatsapp":c.whatsapp,"instagram":c.instagram,"amount":c.amount,
            "links":[{"id":l.id,"title":l.title,"url":l.url,"icon":l.icon,"sort_order":l.sort_order,"active":l.active} for l in c.links]}

@app.route("/api/clients", methods=["POST"])
@login_required
def create_client():
    d=request.get_json() or {}
    name=d.get("name","").strip(); pix=d.get("pix_key","").strip()
    if not name or not pix: return jsonify(error="Nome e chave PIX são obrigatórios"),400
    slug=slugify(d.get("slug") or name)
    if Client.query.filter_by(slug=slug).first(): return jsonify(error="Slug já existe"),409
    c=Client(name=name,slug=slug,pix_key=pix,merchant=d.get("merchant"),city=d.get("city"),
             whatsapp=d.get("whatsapp"),instagram=d.get("instagram"),amount=d.get("amount"))
    db.session.add(c); db.session.commit()
    return jsonify(id=c.id,slug=c.slug),201

@app.route("/api/clients/<int:cid>", methods=["PUT","DELETE"])
@login_required
def modify_client(cid):
    c=Client.query.get_or_404(cid)
    if request.method=="DELETE":
        db.session.delete(c); db.session.commit(); return jsonify(ok=True)
    d=request.get_json() or {}
    for f in ["name","slug","pix_key","merchant","city","whatsapp","instagram","amount"]:
        if f in d: setattr(c,f,d[f])
    db.session.commit(); return jsonify(ok=True)

@app.route("/api/clients/<int:cid>/links", methods=["POST"])
@login_required
def add_link(cid):
    c=Client.query.get_or_404(cid); d=request.get_json() or {}
    try: u=clean_url(d.get("url"))
    except ValueError as e: return jsonify(error=str(e)),400
    l=Link(client_id=c.id,title=d.get("title","Link"),url=u,icon=d.get("icon","🔗"),
           sort_order=d.get("sort_order",0),active=True)
    db.session.add(l); db.session.commit(); return jsonify(id=l.id),201

@app.route("/api/links/<int:lid>", methods=["PUT","DELETE"])
@login_required
def modify_link(lid):
    l=Link.query.get_or_404(lid)
    if request.method=="DELETE":
        db.session.delete(l); db.session.commit(); return jsonify(ok=True)
    d=request.get_json() or {}
    try: u=clean_url(d.get("url"))
    except ValueError as e: return jsonify(error=str(e)),400
    l.title=d.get("title","Link"); l.url=u; l.icon=d.get("icon","🔗")
    l.sort_order=d.get("sort_order",0); l.active=bool(d.get("active",True))
    db.session.commit(); return jsonify(ok=True)

@app.route("/api/clients/<int:cid>/stats")
@login_required
def stats(cid):
    Client.query.get_or_404(cid)
    rows=Access.query.filter_by(client_id=cid).order_by(Access.created_at.desc()).limit(500).all()
    return jsonify(total=Access.query.filter_by(client_id=cid).count(),
                   recent=[{"source":r.source,"created_at":r.created_at.isoformat() if r.created_at else None} for r in rows])

@app.route("/health")
def health(): return "ok"

if __name__=="__main__":
    init_db()
    app.run(host="0.0.0.0",port=int(os.environ.get("PORT",5000)),debug=False)
