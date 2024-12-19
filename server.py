import numpy as np
import faiss
from flask import Flask, request, jsonify
from sentence_transformers import SentenceTransformer
import os

app = Flask(__name__)

folder_path = "./structures"
model = SentenceTransformer('all-MiniLM-L6-v2')
faiss_index = faiss.IndexFlatL2(384)

filenames = [d for d in os.listdir(folder_path)]

print(filenames)
filenames_embeddings = model.encode(filenames)

filenames_embeddings = np.array(filenames_embeddings).astype('float32')

faiss_index.add(filenames_embeddings)

@app.route("/search", methods=["POST"])
def search():
    data = request.json
    prompt = data.get('prompt')
    
    if not prompt:
        return jsonify({"error": "No prompt provided"}), 400
    
    prompt_embedding = model.encode([prompt])
    prompt_embedding = np.array(prompt_embedding).astype('float32')
    D, I = faiss_index.search(prompt_embedding, 1)
    closest_filename = filenames[I[0][0]]
    return jsonify({"closest_filename": closest_filename})

if __name__ == "__main__":
    app.run(debug=True)
